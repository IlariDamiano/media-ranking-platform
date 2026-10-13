import { BrowserRouter, Route, Routes } from "react-router";
import LoginPage from "../pages/LoginPage";
import SiteLayout from "../layouts/SiteLayout";
import DiscoverPage from "../pages/DiscoverPage";
import PlaceholderPage from "../pages/PlaceholderPage";
import RegisterPage from "../pages/RegisterPage";
import { AuthProvider } from "../context/AuthContext";
import RequireAuth from "../components/RequireAuth";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<SiteLayout />}>
            <Route index element={<DiscoverPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route element={<RequireAuth />}>
              <Route
                path="library"
                element={
                  <PlaceholderPage
                    title="YOUR LIBRARY."
                    description="Your ratings, progress and saved stories will live here."
                  />
                }
              />

              <Route
                path="lists"
                element={
                  <PlaceholderPage
                    title="YOUR COLLECTIONS."
                    description="Create, organize and share media rankings with friends."
                  />
                }
              />
            </Route>
            <Route
              path="*"
              element={
                <PlaceholderPage
                  title="404 / LOST IN THE ARCHIVE."
                  description="This page doesn't exist."
                />
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
